import json
import sys

functionsfile, iofile, lockfile = sys.argv[1:]

# Read all the user def. fns.
with open(functionsfile, 'r') as f:
    exec(f.read())

while(True):
    # with open(funlockfile) as f:
    #     funlock = f.read()
    # if funlock and int(funlock):
    #     with open(functionsfile, 'r') as f:
    #         exec(f.read())
    # with open(funlockfile) as f:
    #     funlockfile.write(str(0))
        
    with open(lockfile, 'r') as f:
        lock = f.read()
    
    if lock and int(lock):
        
        with open(iofile, 'r') as f:
            cmd = f.read()

        output = eval(cmd)
        output_json = json.dumps(output)

        with open(iofile, 'w+') as f:
            f.write(output_json)
        
        with open(lockfile, 'w+') as f:
            f.write(str(0))