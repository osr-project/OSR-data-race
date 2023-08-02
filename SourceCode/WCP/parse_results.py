import os
import sys
import ntpath

def get_result(x):
    pieces=x.split('=')
    return ((pieces[1]).split(' '))[1]

def to_sec(x):
    xint=int(x)
    if xint < 1000:
        return "." + str(x[:1])
    else:
        sec=xint/1000
        return str(sec)

def read_time(filename):
    f=open( dirname + "/"  + "/" + filename, "r")
    contents = f.read()
    if contents == "":
        return "-"

    pieces=contents.split('.')
    return pieces[0] 

print('Parsing results file')

results_dir=sys.argv[1]
result_list=[]

for dirname, dirnames, filenames in os.walk(results_dir): 
    test_dir=ntpath.basename(dirname)
    result=""
    if test_dir != ntpath.basename(results_dir):   
        test_name=test_dir.replace("_", "").title()
        
        if test_name == "Longdeadlock":
            test_name="LongDeadlock"

        if test_name == "Arraylist":
            test_name="ArrayList"

        if test_name == "Dinningphil":
            test_name="DinningPhil"

        result=result + test_name + ' & '
        
        ##### META INFO #####
        
        f=open( dirname + "/"  + "/" + "MetaInfo.txt", "r")
        contents = f.read()
        lines = str.splitlines(contents)

        for line in lines: 

            if line.startswith("Number of events"):
                num_events=get_result(line)

            if line.startswith("Number of threads"):
                num_threads=get_result(line)

            if line.startswith("Number of locks"):
                num_locks=get_result(line)

        result=result + num_events + ' & ' + num_threads + ' & ' + num_locks + ' & '
        
        ##### GOODLOCK #####

        f=open( dirname + "/"  + "/" + "Goodlock2.txt", "r")
        contents = f.read()
        lines = str.splitlines(contents)

        for line in lines: 

            if line.startswith("Number of deadlock patterns (of size 2) found"):
                goodlock_num_deadlocks=get_result(line)

            if line.startswith("Time for analysis"):
                goodlock_runtime=to_sec(get_result(line))

        #Comment for self reported time
        #goodlock_runtime=read_time("Goodlock2_time.txt")  

        if contents == "":
            goodlock_num_deadlocks="-"

        result=result + goodlock_runtime + ' & ' + goodlock_num_deadlocks + ' & '

        ##### DIRK #####

        f=open( dirname + "/"  + "/" + "deadlocks.txt", "r")
        contents = f.read()
        lines = str.splitlines(contents)
        
        dirk_num_deadlocks=str(len(lines))      
        dirk_time=read_time("deadlocks_time.txt")        

        result=result + dirk_time + ' & ' + dirk_num_deadlocks +  ' & '

        ##### RCP #####

        f=open( dirname + "/"  + "/" + "RCP.txt", "r")
        contents = f.read()
        lines = str.splitlines(contents)

        for line in lines: 

            if line.startswith("Number of deadlocks found"):
                rcp_num_deadlocks=get_result(line)

            if line.startswith("Time for analysis"):
                rcp_runtime=to_sec(get_result(line))

        #Comment for self reported time
        #rcp_runtime=read_time("RCP_time.txt")
  
        if contents == "":
            rcp_num_deadlocks="-"
            rcp_runtime="-"

        result=result + rcp_runtime + ' & ' + rcp_num_deadlocks + ' & ' 

        ##### RCPDF #####

        f=open( dirname + "/"  + "/" + "RCPDF.txt", "r")
        contents = f.read()

        lines = str.splitlines(contents)

        for line in lines: 

            if line.startswith("Number of deadlocks found"):
                rcpdf_num_deadlocks=get_result(line)
 
            if line.startswith("Time for analysis"):
                rcpdf_runtime=to_sec(get_result(line))


        #Comment for self reported time
        #rcpdf_runtime=read_time("RCPDF_time.txt")  

        if contents == "":
            rcpdf_num_deadlocks="-"
            rcpdf_runtime="-"

        result=result + rcpdf_runtime + ' & ' + rcpdf_num_deadlocks + ' \\\ '

        result_list.append(result)
 
for val in sorted(result_list):
    print(val)



