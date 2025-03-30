# Script to run Voyager simulations
import os
import argparse
import datetime
import multiprocessing as mp
import pandas as pd
import subprocess
import re

VOYAGER_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "../../generators/voyager")
)
ACCELERATOR_DIR = os.path.join(VOYAGER_DIR, "accelerator")


def print_test_results(test_results, layers, output_folder):
    columns = ["Model", "Layer", "Status", "Runtime", "Ideal", "RuntimeType"]
    if len(test_results[0]) == 3:
        columns = columns[:3]

    # convert list of tuples to DataFrame
    df = pd.DataFrame(test_results, columns=columns)
    sorted_df = []

    # get models
    models = df["Model"].unique()

    for model in models:
        print("=" * 10 + f" {model} " + "=" * 10)

        model_df = df[df["Model"] == model]

        # sort according to order in layers
        model_df["Layer"] = pd.Categorical(model_df["Layer"], layers[model])
        model_df.sort_values("Layer", inplace=True)
        # turn categorial back to string
        model_df["Layer"] = model_df["Layer"].astype(str)
        sorted_df.append(model_df)

        passed = model_df[model_df["Status"] == True]
        failed = model_df[model_df["Status"] == False]

        print("Passed:")
        print(
            passed["Layer"].to_string(index=False) if not passed.empty else "None",
            flush=True,
        )
        print("Failed:")
        print(
            failed["Layer"].to_string(index=False) if not failed.empty else "None",
            flush=True,
        )

        # if runtime column exists, print runtime of each layer
        if "Runtime" in model_df.columns:
            print("Runtime:")
            print(
                model_df[["Layer", "Runtime", "Ideal", "RuntimeType"]].to_string(
                    index=False
                ),
                flush=True,
            )
            utilization = model_df["Ideal"].sum() / model_df["Runtime"].sum()
            matrix_runtime = model_df[model_df["RuntimeType"] == "matrix"][
                "Runtime"
            ].sum()
            matrix_ideal = model_df[model_df["RuntimeType"] == "matrix"]["Ideal"].sum()
            matri_utilization = matrix_ideal / matrix_runtime
            print(f"Utilization: {utilization:.3f}")
            print(f"Matrix Utilization: {matri_utilization:.3f}")

    # concatentate all sorted model DataFrames into a single DataFrame and save to pickle
    pd.concat(sorted_df).to_pickle(f"{output_folder}/test_results.pkl")

    # return True if all tests passed
    return len(df[df["Status"] == False]) == 0


def run_rtl_simulation(model, layer, output_folder, env_vars, config_path, debug):
    env_vars["NETWORK"] = model
    env_vars["TESTS"] = layer

    with open(f"{output_folder}/{model}_{layer}.log", "w") as log_file, open(
        f"{output_folder}/{model}_{layer}.err", "w"
    ) as err_file:
        subprocess.run(
            [
                (
                    f"./simv-chipyard.harness-{env_vars['CONFIG']}"
                    if not debug
                    else f"./simv-chipyard.harness-{env_vars['CONFIG']}-debug"
                ),
                "+permissive",
                "+dramsim",
                "+dramsim_ini_dir=../../generators/testchipip/src/main/resources/dramsim2_ini",
                "+max-cycles=10000000",
                "+ntb_random_seed_automatic",
                "+verbose",
                f"+fsdbfile={output_folder}/{model}_{layer}.fsdb" if debug else "",
                "+permissive-off",
                f"../../tests/voyager/networks/{model}/{config_path}/{layer}.riscv",
            ],
            env=env_vars,
            stdout=log_file,
            stderr=err_file,
            timeout=2 * 60 * 60,  # 2 hours
        )

    # search if the test passed
    p = subprocess.Popen(
        ["grep", "Error count: 0", f"{output_folder}/{model}_{layer}.log"],
        stdout=subprocess.PIPE,
    )
    p.communicate()
    success = p.returncode == 0

    if success:
        # capture ideal runtime number in the log file
        # can either be "Matrix unit ideal runtime: " or "Vector unit ideal runtime: "
        p = subprocess.Popen(
            [
                "grep",
                "-oP",
                "(?<=matrix unit ideal runtime: ).\d+",
                f"{output_folder}/{model}_{layer}.log",
            ],
            stdout=subprocess.PIPE,
        )

        match = p.communicate()[0]

        if match:
            runtime_type = "matrix"
        else:
            p = subprocess.Popen(
                [
                    "grep",
                    "-oP",
                    "(?<=vector unit ideal runtime: ).\d+",
                    f"{output_folder}/{model}_{layer}.log",
                ],
                stdout=subprocess.PIPE,
            )
            match, error = p.communicate()
            assert not error

            runtime_type = "vector"

        ideal = int(match.decode("utf-8").strip())

        # capture runtime number in the log file
        if runtime_type == "matrix":
            search_text = "Accelerator Runtime: "
        else:
            search_text = "Vector Unit Runtime: "
        p = subprocess.Popen(
            [
                "grep",
                "-oP",
                f"(?<={search_text}).\d+",
                f"{output_folder}/{model}_{layer}.log",
            ],
            stdout=subprocess.PIPE,
        )
        runtime = int(p.communicate()[0].decode("utf-8").strip())
    else:
        runtime = 0
        ideal = 0
        runtime_type = ""

    return (model, layer, success, runtime, ideal, runtime_type)


def run_rtl_simulations(
    layers, num_processes, results_folder, env_vars, config_path, debug
):
    pool = mp.Pool(num_processes)

    test_results = []

    for model, tests in layers.items():
        for test in tests:
            pool.apply_async(
                run_rtl_simulation,
                args=(model, test, results_folder, env_vars, config_path, debug),
                callback=test_results.append,
            )
    pool.close()
    pool.join()

    return print_test_results(test_results, layers, results_folder)


def main():
    argparser = argparse.ArgumentParser(description="Run Voyager simulations")
    argparser.add_argument(
        "--models",
        required=True,
        help="Model(s) to test for regression (resnet18, mobilebert)",
    )
    argparser.add_argument(
        "--tests",
        default=None,
        help="Comma separated list of tests to run (e.g. test1,test2)",
    )
    argparser.add_argument(
        "--num_processes",
        type=int,
        required=True,
        help="Number of processes to run in parallel",
    )
    argparser.add_argument(
        "--debug",
        action="store_true",
        help="Run the simulation in debug mode to generate waveforms",
    )
    argparser.add_argument("--config", required=True, help="SoC config name")
    args = argparser.parse_args()
    args.models = [s.strip() for s in args.models.split(",")]

    # Create directory with current time
    current_time = datetime.datetime.now().strftime("%Y_%m_%d_%H_%M_%S")
    results_folder = "regression_results/" + current_time
    os.makedirs(results_folder)
    # create softlink to latest results (delete old if exists)
    os.system("rm -f regression_results/latest")
    os.system(f"cd regression_results && ln -sf {current_time} latest")

    # Set environment variables
    env_vars = os.environ.copy()
    env_vars["CONFIG"] = args.config
    env_vars["PROJECT_ROOT"] = ACCELERATOR_DIR
    env_vars["CODEGEN_DIR"] = "test/compiler"
    env_vars["NETWORK"] = args.models[0]
    # Set the memory banking configuration for the compiler
    env_vars["EXTRA_COMPILER_FLAGS"] = "--bank_width 256"

    # Generate SoC verilog
    print("Generating verilog...")
    with open(f"{results_folder}/verilog_generation.log", "w") as log_file:
        subprocess.run(
            ["make", "verilog"],
            env=env_vars,
            stdout=log_file,
            stderr=subprocess.STDOUT,
        )

    # Set the environment variables from the sh file
    env_file = f"{ACCELERATOR_DIR}/build/configs/{args.config}/config.sh"
    with open(env_file, "r") as f:
        for line in f:
            match = re.match(r"export\s+(\w+)=(.*)", line.strip())
            if match:
                var, val = match.groups()
                # Remove surrounding quotes if present
                val = val.strip("'\"")
                env_vars[var] = val

    # Compile the model(s) and get list of layers
    layers = {}
    if args.tests is None:
        for network in args.models:
            env_vars["NETWORK"] = network
            subprocess.run(
                ["make", "-C", ACCELERATOR_DIR, "network-proto"],
                env=env_vars,
            )
            with open(
                f"{ACCELERATOR_DIR}/test/compiler/networks/{network}/{env_vars['DATATYPE']}/layers.txt",
                "r",
            ) as f:
                layers[network] = f.read().splitlines()
    else:
        assert (
            len(args.models) == 1
        ), "Only one model can be specified when using --tests"
        layers[args.models[0]] = args.tests.split(",")

    config_path = f"{env_vars['DATATYPE']}/{env_vars['IC_DIMENSION']}x{env_vars['OC_DIMENSION']}_{env_vars['INPUT_BUFFER_SIZE']}x{env_vars['WEIGHT_BUFFER_SIZE']}x{env_vars['ACCUM_BUFFER_SIZE']}"

    # Build the simulation binary
    print("Building the simulation binary...")
    with open(f"{results_folder}/sim_build.log", "w") as log_file:
        subprocess.run(
            ["make", "default" if not args.debug else "debug"],
            env=env_vars,
            stdout=log_file,
            stderr=subprocess.STDOUT,
        )

    # Generate the SoC C driver code
    print("Generating the SoC C driver code...")
    with open(f"{results_folder}/driver_code_gen.log", "w") as log_file:
        for network in args.models:
            env_vars["NETWORK"] = network
            subprocess.run(
                [
                    "make",
                    "-C",
                    f"{VOYAGER_DIR}/software/",
                    "GenerateSoCBinaries",
                ],
                env=env_vars,
                stdout=log_file,
                stderr=subprocess.STDOUT,
            )

            for layer in layers[network]:
                # Compile the SoC C code
                subprocess.run(
                    [
                        "make",
                        "-C",
                        "../../tests/voyager/",
                        f"networks/{network}/{config_path}/{layer}.riscv",
                    ],
                    env=env_vars,
                    stdout=log_file,
                    stderr=subprocess.STDOUT,
                )

    success = run_rtl_simulations(
        layers,
        args.num_processes,
        results_folder,
        env_vars,
        config_path,
        args.debug,
    )

    exit(0 if success else 1)


if __name__ == "__main__":
    main()
