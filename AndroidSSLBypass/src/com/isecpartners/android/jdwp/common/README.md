The code in this directory is the basis of inter-thread communication. 
All Threads implement the QueueAgent in order to communicate with one
another. Each thread has an in-Queue from which they receive messages
and an out-Queue to which they send messages. QueueAgents can register
other QueuAgents as listeners in order to send and receive messages
from them.
