import os
import platform
diamond=os.environ['INTERNAL_DIAMOND_PATH']
os.system('%s/bin/nt64/pgrcmd -infile  test.xcf    >  ../_scratch/ProgrammerLog.txt '% diamond)
