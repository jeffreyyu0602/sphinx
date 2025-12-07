# Script to run Voyager simulations
import os
import argparse
import datetime
import json
import math
import multiprocessing as mp
import numpy as np
import pandas as pd
import subprocess
import re
from deepdiff import DeepDiff
from google.protobuf import text_format
from google.protobuf.json_format import MessageToDict
from quantized_training.codegen import param_pb2

VOYAGER_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "../../generators/voyager")
)
ACCELERATOR_DIR = os.path.join(VOYAGER_DIR, "accelerator")


def utilization(df):
    count = df["Count"].to_numpy()
    full_tiles = df["L2 Tiles"].to_numpy()
    actual_tiles = df["Actual Tiles"].to_numpy()
    ideal = df["Ideal"].to_numpy()
    runtime = df["Runtime"].to_numpy()

    # Precompute common factor
    weight = (full_tiles * count) / actual_tiles

    numerator = np.sum(ideal * weight)
    denominator = np.sum(runtime * weight)

    return numerator / denominator if denominator != 0 else np.nan


def print_test_results(test_results, layers, output_folder):
    columns = [
        "Model",
        "Layer",
        "Status",
        "Runtime",
        "Ideal",
        "RuntimeType",
        "Count",
        "L2 Tiles",
        "Actual Tiles",
    ]
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
        print(passed["Layer"].to_string(index=False) if not passed.empty else "None")
        print("Failed:")
        print(failed["Layer"].to_string(index=False) if not failed.empty else "None")

        # if runtime column exists, print runtime of each layer
        if "Runtime" in model_df.columns:
            print("Runtime:")
            print(model_df[columns[1:]].to_string(index=False), flush=True)

            utilization_all    = utilization(model_df)
            utilization_matrix = utilization(model_df[model_df["RuntimeType"] == "matrix"])

            print(f"Utilization: {utilization_all:.3f}")
            print(f"Matrix Utilization: {utilization_matrix:.3f}")

    # concatentate all DataFrames into a single DataFrame and save to pickle and excel
    combined_df = pd.concat(sorted_df)
    combined_df.to_pickle(f"{output_folder}/test_results.pkl")
    combined_df.to_excel(f"{output_folder}/test_results.xlsx", index=False)

    # return True if all tests passed
    return len(df[df["Status"] == False]) == 0


def run_rtl_simulation(
    model,
    layer,
    layer_count,
    tile_count,
    output_folder,
    env_vars,
    config_path,
    debug,
):
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
                f"+fsdbfile=dump.fsdb" if debug else "",
                "+permissive-off",
                f"../../tests/voyager/networks/{model}/{config_path}/{layer}.riscv",
            ],
            env=env_vars,
            stdout=log_file,
            stderr=err_file,
            timeout=2 * 60 * 60,  # 2 hours
        )

    log_file = f"{output_folder}/{model}_{layer}.log"

    success = False
    runtime = 0
    ideal = 0
    runtime_type = ""

    if os.path.exists(log_file):
        with open(log_file, "r") as file:
            content = file.read()

        # 1. Check if test passed
        success = bool(re.search(r"Error\s+count:\s+0", content))
        
        if success:
            # 2. Capture runtime type (Matrix or Vector) and ideal runtime
            match = re.search(
                r"(matrix|vector)\s+unit\s+ideal\s+runtime:\s+(\d+)\s*ns",
                content,
                flags=re.IGNORECASE,
            )
            if match:
                runtime_type = match.group(1).lower()  # "matrix" or "vector"
                ideal = int(match.group(2))

            # 3. Capture runtime based on type
            if runtime_type == "matrix":
                runtime_match = re.search(
                    r"Accelerator\s+Runtime:\s+(\d+)\s*cycles", content, flags=re.IGNORECASE
                )
            elif runtime_type == "vector":
                runtime_match = re.search(
                    r"Vector\s+Unit\s+Runtime:\s+(\d+)\s*cycles", content, flags=re.IGNORECASE
                )
            else:
                runtime_match = None

            if runtime_match:
                runtime = int(runtime_match.group(1))

    # Calculate actual tiles (min of L2 tiles and max_tiles)
    max_tiles = int(os.environ.get("MAX_TILES", "1"))
    actual_tiles = min(tile_count, max_tiles)

    return (
        model,
        layer,
        success,
        runtime,
        ideal,
        runtime_type,
        layer_count,
        tile_count,
        actual_tiles,
    )


def run_rtl_simulations(
    layers,
    layer_counts,
    tile_counts,
    num_processes,
    results_folder,
    env_vars,
    config_path,
    debug
):
    pool = mp.Pool(num_processes)

    test_results = []

    for model, tests in layers.items():
        for test in tests:
            pool.apply_async(
                run_rtl_simulation,
                args=(
                    model,
                    test,
                    layer_counts[model][test],
                    tile_counts[model][test],
                    results_folder,
                    env_vars,
                    config_path,
                    debug,
                ),
                callback=test_results.append,
            )
    pool.close()
    pool.join()

    return print_test_results(test_results, layers, results_folder)


def add_layers(network, layers, layer_counts, tile_counts, uniquify, skip_layers, env_vars):
    layers[network] = []
    layer_counts[network] = {}
    tile_counts[network] = {}

    skip_layers = [re.compile(p) for p in skip_layers] if skip_layers else []

    if not uniquify:
        with open(
            f"{ACCELERATOR_DIR}/test/compiler/networks/{network}/{env_vars['DATATYPE']}/layers.txt",
            "r",
        ) as f:
            all_layers = f.read().splitlines()
            # Filter out layers that should be skipped
            layers[network] = [
                layer for layer in all_layers
                if not any(p.match(layer) for p in skip_layers)
            ]
            layer_counts[network] = {layer: 1 for layer in layers[network]}
    else:
        # open the proto file
        with open(
            f"{ACCELERATOR_DIR}/test/compiler/networks/{network}/{env_vars['DATATYPE']}/model.txt",
            "r",
        ) as f:
            contents = f.read()
        params = param_pb2.Model()
        text_format.Parse(contents, params)

        # convert to json
        params_dict = MessageToDict(params, preserving_proto_field_name=True)

        def delete_nested_keys(data, key):
            if isinstance(data, dict):
                for k in list(data.keys()):
                    if k == key:
                        del data[k]
                    else:
                        delete_nested_keys(data[k], key)
            elif isinstance(data, list):
                for item in data:
                    delete_nested_keys(item, key)

        unique_layers = {}
        for op in params_dict["ops"]:
            # skip nop layers
            if "op" in op and op["op"]["op"] == "nop":
                continue

            name = op["op"]["name"] if "op" in op else op["fused_op"]["name"]

            # Skip layers that are in the skip list
            if any(p.match(name) for p in skip_layers):
                continue

            # remove the name, memory, and node fields from the op
            delete_nested_keys(op, "name")
            delete_nested_keys(op, "memory")
            delete_nested_keys(op, "scratchpad")
            delete_nested_keys(op, "node")

            if "op" in op:
                kwargs = op["op"]["kwargs"]
            else:
                kwargs = op["fused_op"]["op_list"][0]["kwargs"]

            l2_values = kwargs.get("l2_tiling", {}).get("int_list", {}).get("values", [])
            l2_values = [int(v) for v in l2_values]
            l2_count = math.prod(l2_values) if l2_values else 1

            is_unique_layer = True
            for op_name, op_val in unique_layers.items():
                if not DeepDiff(op, op_val, ignore_order=True):
                    layer_counts[network][op_name] += 1
                    is_unique_layer = False
                    break

            if is_unique_layer:
                unique_layers[name] = op
                layers[network].append(name)
                layer_counts[network][name] = 1
                tile_counts[network][name] = l2_count


def matches(value, rule_value):
    if isinstance(rule_value, list):
        return value in rule_value
    else:
        if rule_value == "*":
            return True
        else:
            return value == rule_value


def get_skip_layers(skip_rules, model, datatype, sim_type, block_size):
    best_rule = None
    best_specificity = -1

    for rule in skip_rules:
        if (
            matches(model, rule["model"])
            and matches(datatype, rule["datatype"])
            and matches(sim_type, rule["sim_type"])
            and matches(block_size, rule["block_size"])
        ):
            # Calculate specificity score (higher is more specific)
            # Exact match = 2, list match = 1, wildcard = 0
            specificity = 0
            specificity += 2 if rule["datatype"] != "*" else 0
            specificity += 1 if isinstance(rule["model"], list) else (2 if rule["model"] != "*" else 0)
            specificity += 1 if isinstance(rule["sim_type"], list) else (2 if rule["sim_type"] != "*" else 0)
            specificity += 2 if rule["block_size"] != "*" else 0

            if specificity > best_specificity:
                best_specificity = specificity
                best_rule = rule

    return set(best_rule["layers"]) if best_rule else set()


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
    argparser.add_argument(
        "--uniquify_layers",
        action="store_true",
        help="Whether to remove duplicated layers in the model",
    )
    argparser.add_argument(
        "--skip_layers",
        action="store_true",
        help="Whether to skip layers specified in the skip_rules.json",
    )
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

    # Default to 8MB of scratchpad memory with 8 banks
    cache_size = int(env_vars.setdefault("CACHE_SIZE", "8388608"))
    num_banks = int(env_vars.setdefault("NUM_BANKS", "8"))

    # Default to use the same scratchpad configuration as the accelerator
    env_vars.setdefault("SOC_CACHE_SIZE", str(2 * cache_size))
    env_vars.setdefault("SOC_NUM_BANKS", str(2 * num_banks))

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
    defines = []
    with open(f"{ACCELERATOR_DIR}/build/configs/{args.config}/config.sh", "r") as f:
        for line in f:
            if (match := re.match(r"export\s+(\w+)=(.*)", line.strip())) is None:
                continue
            var, val = match.groups()
            val = val.strip("'\"")
            env_vars[var] = val
            defines.append(f"-D{val}" if var == "DATATYPE" else f"-D{var}={val}")
    env_vars.setdefault("EXTRA_SIM_CXXFLAGS", " ".join(defines))

    # Set the memory banking configuration for the compiler
    # accesses need to be aligned to accelerator's data width
    # width of datatype * 2 * max(IC,OC)
    if env_vars["DATATYPE"] == "E4M3":
        datatype_width = 8
    elif env_vars["DATATYPE"] == "INT8":
        datatype_width = 8
    elif env_vars["DATATYPE"] == "MXINT8":
        datatype_width = 8
    elif env_vars["DATATYPE"] == "MXNF4":
        datatype_width = 4
    else:
        raise ValueError(
            f"Unknown width for DATATYPE: {env_vars['DATATYPE']}. Please add it to run_voyager.py"
        )

    bank_width = int(
        datatype_width
        * min(int(env_vars["IC_DIMENSION"]), int(env_vars["OC_DIMENSION"]))
        / 8
    )

    env_vars.setdefault("BANK_WIDTH", str(bank_width))
    env_vars.setdefault("EXTRA_COMPILER_FLAGS", f"--bank_width {bank_width}")
    env_vars.setdefault("SOC_SIM", "1")
    env_vars.setdefault("SPDLOG_LEVEL", "DEBUG")

    # Compile the model(s) and get list of layers
    layers = {}
    layer_counts = {}
    tile_counts = {}
    skip_rules = []

    if args.skip_layers:
        with open(f"{ACCELERATOR_DIR}/ci_skip_rules.json", "r") as f:
            skip_rules = json.load(f)

    if args.tests is None:
        for network in args.models:
            env_vars["NETWORK"] = network
            subprocess.run(
                ["make", "-C", ACCELERATOR_DIR, "network-proto"],
                env=env_vars,
            )
            datatype = env_vars["DATATYPE"]
            block_size = max(
                int(env_vars["OC_DIMENSION"]), int(env_vars["IC_DIMENSION"])
            )
            skip_layers = get_skip_layers(
                skip_rules, network, datatype, "rtl", block_size
            )
            add_layers(
                network,
                layers,
                layer_counts,
                tile_counts,
                args.uniquify_layers,
                skip_layers,
                env_vars,
            )
    else:
        assert (
            len(args.models) == 1
        ), "Only one model can be specified when using --tests"
        layers[args.models[0]] = args.tests.split(",")
        layer_counts[args.models[0]] = {layer: 1 for layer in layers[args.models[0]]}
        tile_counts[args.models[0]] = {test: 1 for test in layers[args.models[0]]}

    config_path = (
        f"{env_vars['DATATYPE']}/"
        f"{env_vars['IC_DIMENSION']}x{env_vars['OC_DIMENSION']}_"
        f"{env_vars['INPUT_BUFFER_SIZE']}x{env_vars['WEIGHT_BUFFER_SIZE']}x{env_vars['ACCUM_BUFFER_SIZE']}_"
        f"{env_vars['DOUBLE_BUFFERED_ACCUM_BUFFER']}_"
        f"{env_vars['SUPPORT_MVM']}_"
        f"{env_vars['SUPPORT_SPMM']}"
    )

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
        layer_counts,
        tile_counts,
        args.num_processes,
        results_folder,
        env_vars,
        config_path,
        args.debug,
    )

    exit(0 if success else 1)


if __name__ == "__main__":
    main()
