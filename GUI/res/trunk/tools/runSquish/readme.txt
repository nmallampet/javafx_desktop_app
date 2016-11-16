python copy_suites.py
// copy suites to local folder
// modify the directory names

python gen_lines.py > run_all_squish.lines
// generate the batch lines file
// if found .conf and .map in a folder, it will be treated as a suite path
// all tst_xx will be treated as a case

python run_squish.py
// launch test flow

TIPS:
  How to launch the commands?
    1. change .lines to .bat and click it
    2. python tools\runLines\run_lines.py

