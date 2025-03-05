# Script to run Voyager simulations
import os
import argparse
import subprocess


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
    args = argparser.parse_args()
    args.models = [s.strip() for s in args.models.split(",")]

    # Set environment variables
    env_vars = os.environ.copy()
    env_vars["PROJECT_ROOT"] = os.path.abspath("../../generators/voyager/accelerator/")
    env_vars["CODEGEN_DIR"] = "test/compiler"
    env_vars["TESTS"] = args.tests
    if "INPUT_BUFFER_SIZE" not in env_vars:
        env_vars["INPUT_BUFFER_SIZE"] = "1024"
    if "WEIGHT_BUFFER_SIZE" not in env_vars:
        env_vars["WEIGHT_BUFFER_SIZE"] = "1024"
    if "ACCUM_BUFFER_SIZE" not in env_vars:
        env_vars["ACCUM_BUFFER_SIZE"] = "1024"

    config_path = f"{env_vars['DATATYPE']}/{env_vars['IC_DIMENSION']}x{env_vars['OC_DIMENSION']}_{env_vars['INPUT_BUFFER_SIZE']}x{env_vars['WEIGHT_BUFFER_SIZE']}x{env_vars['ACCUM_BUFFER_SIZE']}"

    # Compile networks
    print("Compiling networks...")
    for network in args.models:
        env_vars["NETWORK"] = network
        subprocess.run(
            ["make", "-C", "../../generators/voyager/accelerator/", "network-proto"],
            env=env_vars,
        )

    # Generate the SoC C code
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

    # Build the simulation binary
    print("Building the simulation binary")
    subprocess.run(
        [
            "make",
            "-f",
            "voyager.mk",
            "sim" if not args.debug else "sim-debug",
        ],
        env=env_vars,
    )

    # Run the simulation
    print("Running the simulation")
    with open("sim.log", "w") as log_file, open("sim.err", "w") as err_file:
        subprocess.run(
            [
                (
                    "./simv-chipyard.harness-VoyagerConfig"
                    if not args.debug
                    else "./simv-chipyard.harness-VoyagerDebugConfig-debug"
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
