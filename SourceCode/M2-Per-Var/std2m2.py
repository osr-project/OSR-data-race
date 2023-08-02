from __future__ import division
import sys,os,glob
from itertools import chain

# Convert std file to m2 with a header showing the number of events in each thread, with hb relations instead of forks and joins
# We only output threads that perform visible operations
# Removes nested locking and open critical sections
# python std2m2.py traces/airlinetickets.std

def get_event_info(line, local_traces, locals2ints, threads2ints):

	parts = line.strip().split('|')
	
	if parts[0] not in threads2ints: 
		threads2ints[parts[0]] = len(threads2ints)
		local_traces.append([])
	t = threads2ints[parts[0]]

	op = ''
	if 'acq(' in parts[1]: op = 'acq'
	elif 'rel(' in parts[1]: op = 'rel'
	elif 'w(' in parts[1]: op = 'w'
	elif 'r(' in parts[1]: op= 'r'
	elif 'fork(' in parts[1]: op='fork'
	elif 'join(' in parts[1]: op='join'

	v=-1
	if op == 'fork' or op == 'join':
		l = parts[1].split('(')[1].split(')')[0]
		if l not in threads2ints:
			threads2ints[l] = len(threads2ints)
			local_traces.append([])
		v = threads2ints[l]

	else:	
		l = parts[1].split('(')[1].split(')')[0]
		if l not in locals2ints: locals2ints[l] = len(locals2ints) + 1
		v = locals2ints[l]

	l_number = int(parts[2])

	return t, op, v, l_number



def get_thread_length(local_traces, t):
	return len(local_traces[t])



def line_is_empty(line):
	return len(line)<3 or line.strip().count(',') == len(line.strip())
	

def get_my_event_info(e):
	parts = e.split('|')
	op, v = parts[0].split(':')
	return op, v



def process_event(line, trace, local_traces, hb, writes, init_writes, locals2ints, threads2ints, forked_threads):
	t, op, v, l_number = get_event_info(line, local_traces, locals2ints, threads2ints)

	#print t, op, v, l_number
	
	if op == 'fork':
		forked_threads.add(v)
		to_e = (v,-1)
		#to_e = (v,0)
		from_e = (t, get_thread_length(local_traces, t))
		hb.add((from_e, to_e))
		trace.append(t)
		local_traces[t].append('noop:-1' + '|' + str(l_number))
		#print 'added fork hb', (from_e, to_e)
	elif op == 'join':
		to_e = (t,get_thread_length(local_traces, t))
		from_e = (v, get_thread_length(local_traces, v)-1)
		hb.add((from_e, to_e))
		trace.append(t)
		local_traces[t].append('noop:-1' + '|' + str(l_number))
		#print 'added join hb', (from_e, to_e)
	else:
		trace.append(t)
		local_traces[t].append(op+':'+str(v) + '|' + str(l_number))
		if op == 'w': 
			writes.add(v)
		elif op == 'r':
			if v not in writes:
				init_writes.add(v)
	

def print_local_traces(local_traces):
	print('-' * 50)
	for t in range(len(local_traces)):
		print('trace', t)
		for e in local_traces[t]: print(e)






def output_trace(filename, trace, local_traces, hb, init_writes, forked_threads):

	#print 'hb', hb

	num_threads = len(local_traces)
	main_thread = -1

	for t in range(num_threads):
		if t not in forked_threads:
			main_thread = t
			break

	trace_lengths = [get_thread_length(local_traces, t)+2 for t in range(len(local_traces))]
	trace_lengths[main_thread] += len(init_writes) + 1
	rev_count = -1
	

	#print 'thread_map', thread_map

	#print 'new_hb', new_hb
	#print_local_traces(local_traces)

	f = open(filename, 'w')
	print('Num of threads:', num_threads, file=f)
	print('Num of events:', ','.join([str(x) for x in trace_lengths]), file=f)

	#Initialize main thread
	#print('noop:-1' + ',' * (num_threads-1), '|', rev_count, ',', rev_count, file=f)
	print((main_thread) * ',' + 'noop:-1' + ',' * (num_threads - main_thread -1), '|', rev_count, ',', rev_count, file=f)
	rev_count -=1
	
	for v in init_writes: print((main_thread) * ',' + 'w:'+str(v) + ',' * (num_threads -main_thread -1), '|', rev_count, ',', rev_count, file=f)
	#Initialize the remaining threads
	#for t in chain(range(main_thread), range(main_thread+1,num_threads)):
	#	print((t) * ',' + 'noop:-1' + ',' * (num_threads-t-1), '|', rev_count, ',', rev_count, file=f)
	#	rev_count -=1

	#Output trace
	trace_indexes = [0] * num_threads
	for i,tp in enumerate(trace):
		t = tp

		if trace_indexes[t] == 0:
			print((t) * ',' + 'noop:-1' + ',' * (num_threads-t-1), '|', rev_count, ',', rev_count, file=f)
			rev_count -=1

		e, l_number = local_traces[t][trace_indexes[t]].split('|')
		print((t) * ',' + e  + ',' * (num_threads-t-1), '|', i, ',', l_number, file=f)

		trace_indexes[t] += 1
	#Finalize all threads	
	for t in range(num_threads):
		print((t) * ',' + 'noop:-1' + ',' * (num_threads-t-1), '|', rev_count, ',', rev_count, file=f)
		if trace_indexes[t] == 0: print((t) * ',' + 'noop:-1' + ',' * (num_threads-t-1), '|', rev_count, ',', rev_count, file=f)
	#Output HB
	for (e_from, e_to) in hb:
		print(str(e_from[0]) + '.' + str(e_from[1]+1) + '-' + str(e_to[0]) + '.' + str(e_to[1]+1), file=f)
	# Additional HB edges from the init_writes	
	for t in range(num_threads):
		if t is not main_thread:
			print(str(main_thread) + '.' + str(len(init_writes)) + '-' + str(t) + '.0', file = f)
	f.close()



def convert_one(filename_in, filename_out):

	print('Converting', filename_in)

	threads2ints, locals2ints = {}, {} # Maps every thread id and local variable/lock to an int
	writes = set()	# The set of locations that are written
	init_writes = set() #The set of locations that are read before written, thus need to add initialization writes
	trace = [] # The global trace is a set of indicies of threads (t1, t2, ...)
	local_traces = []	# The local traces is one array for each local trace, the array consisting of events
	hb = set()	# The happens before edges (t1,i1) -> (t2, i2)
	forked_threads = set()

	fin = open(filename_in)
	for line in fin: 
		if line_is_empty(line): break
		process_event(line, trace, local_traces, hb, writes, init_writes, locals2ints, threads2ints, forked_threads)
	fin.close()

	output_trace(filename_out, trace, local_traces, hb, init_writes, forked_threads)


def convert_dir(dir):

	for filename_in in glob.glob(dir + '/*.std'):
		filename_out = '.'.join(filename_in.split('.')[:-1]) + '.m2'
		convert_one(filename_in, filename_out)


# python std2m2.py traces/airlinetickets.std
def main():

	if '.std' in sys.argv[1]:
		filename_in = sys.argv[1]
		filename_out = '.'.join(filename_in.split('.')[:-1]) + '.m2'
		convert_one(filename_in, filename_out)
	
	else:
		convert_dir(sys.argv[1])


main()

		





	


