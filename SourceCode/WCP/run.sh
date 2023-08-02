#!/bin/bash

declare -a TESTS=("Test")

#declare -a TESTS=( "Bensalem" "DBCP1" "DBCP2" "Deadlock" "Derby2" "Dinning_Phil" \
#                   "HashMap" "Log4j2" "LongDeadlock" "PickLock" "Transfer" "Eclipse" )

LOCATION="../traces/"

for TEST in "${TESTS[@]}"
do
    echo -e "====Running ${TEST}====\n"
#    ./wtt parse ${LOCATION}${TEST}/_wiretap/wiretap.hist > ${LOCATION}${TEST}/log.txt
    java -cp lib/*:rapid.jar WiretapToSTD ${LOCATION}${TEST}/log.txt > ${LOCATION}${TEST}/std.log

    LEN=`cat ${LOCATION}${TEST}/std.log | wc -l `
    echo -e "trace length = ${LEN}"

    #echo -e "\n--dirk--"
    #./wtt deadlocks ${LOCATION}${TEST}/_wiretap/wiretap.hist

    echo -e "\n--RCP--"
    java -cp lib/*:rapid.jar RCP -f std -p ${LOCATION}${TEST}/std.log

    echo -e "\n--RCPDF--"
    java -cp lib/*:rapid.jar RCPDF -f std -p ${LOCATION}${TEST}/std.log

    echo ""

done
