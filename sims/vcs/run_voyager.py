# Script to run Voyager simulations
import os
import argparse
import subprocess
import re


def main():
    argparser = argparse.ArgumentParser(description="Run Voyager simulations")
    argparser.add_argument(
        "--models",
        required=True,
        help="Model(s) to test for regression (resnet18, mobilebert)",
    )
    argparser.add_argument(
        "--tests",
        required=True,
        help="Comma separated list of tests to run (e.g. test1,test2)",
    )
    argparser.add_argument(
        "--debug",
        action="store_true",
        help="Run the simulation in debug mode to generate waveforms",
    )
    argparser.add_argument("--config", required=True, help="SoC config name")
    args = argparser.parse_args()
    args.models = [s.strip() for s in args.models.split(",")]

    # Set environment variables
    env_vars = os.environ.copy()
    env_vars["CONFIG"] = args.config
    env_vars["PROJECT_ROOT"] = os.path.abspath("../../generators/voyager/accelerator/")
    env_vars["CODEGEN_DIR"] = "test/compiler"
    env_vars["TESTS"] = args.tests
    env_vars["NETWORK"] = args.models[0]

    # Generate SoC verilog
    print("Generating verilog...")
    subprocess.run(
        ["make", "verilog"],
        env=env_vars,
    )

    # Set the environment variables from the sh file
    env_file = (
        f"../../generators/voyager/accelerator/build/configs/{args.config}/config.sh"
    )
    with open(env_file, "r") as f:
        for line in f:
            match = re.match(r"export\s+(\w+)=(.*)", line.strip())
            if match:
                var, val = match.groups()
                # Remove surrounding quotes if present
                val = val.strip("'\"")
                env_vars[var] = val
    config_path = (
        env_vars["DATATYPE"]
        + "/"
        + env_vars["IC_DIMENSION"]
        + "x"
        + env_vars["OC_DIMENSION"]
        + "_"
        + env_vars["INPUT_BUFFER_SIZE"]
        + "x"
        + env_vars["WEIGHT_BUFFER_SIZE"]
        + "x"
        + env_vars["ACCUM_BUFFER_SIZE"]
    )

    # Build the simulation binary
    print("Building the simulation binary")
    subprocess.run(
        ["make", "default" if not args.debug else "debug"],
        env=env_vars,
    )

    # Generate the SoC C driver code
    # Set the memory banking configuration for the compiler
    env_vars["EXTRA_COMPILER_FLAGS"] = "--bank_width 256"
    subprocess.run(
        [
            "make",
            "-C",
            "../../generators/voyager/software/",
            "GenerateSoCBinaries",
        ],
        env=env_vars,
    )

    # # Compile the SoC C code
    subprocess.run(
        [
            "make",
            "-C",
            "../../tests/voyager/",
            f"networks/{args.models[0]}/{config_path}/{args.tests}.riscv",
        ],
        env=env_vars,
    )

    # Run the simulation
    print("Running the simulation")
    with open("sim.log", "w") as log_file, open("sim.err", "w") as err_file:
        subprocess.run(
            [
                (
                    f"./simv-chipyard.harness-{args.config}"
                    if not args.debug
                    else f"./simv-chipyard.harness-{args.config}-debug"
                ),
                "+permissive",
                "+dramsim",
                "+dramsim_ini_dir=../../generators/testchipip/src/main/resources/dramsim2_ini",
                "+max-cycles=10000000",
                "+ntb_random_seed_automatic",
                "+verbose",
                f"+fsdbfile=output/{args.tests}.fsdb" if args.debug else "",
                "+permissive-off",
                f"../../tests/voyager/networks/{args.models[0]}/{config_path}/{args.tests}.riscv",
            ],
            env=env_vars,
            stdout=log_file,
            stderr=err_file,
        )


if __name__ == "__main__":
    main()
