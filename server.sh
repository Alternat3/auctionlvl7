cd frontend
rmiregistry &
sleep 2
java FrontEnd &
sleep 2
cd ..
cd replica
java Replica 1 &
java Replica 2 &
java Replica 3 &