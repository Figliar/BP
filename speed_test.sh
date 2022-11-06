#!/bin/bash
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Shimadzu/*"
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Mettler/*"
#FILES="/home/rene/Desktop/Bakalarka/ReFormator/ReFormator/Modules/src/test/resources/test-data/SpecordCSV/*"
FILES="/home/rene/Desktop/Bakalarka/ReFormator/ReFormator/Modules/src/test/resources/test-data/Agilent/*"
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Dalsi_priklady/Letters/*"
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Dalsi_priklady/Agilent/*"
i=0
for f in $FILES
do
  i=$((i+1))
done
start=$(date +%s.%N)
for f in $FILES
do
  echo "Processing $f file..."
  ./parse.sh "$f"
done
dur1=$(echo "$(date +%s.%N) - $start" | bc)

start=$(date +%s.%N)
for f in $FILES
do
  echo "Processing $f file..."
  java -jar ~/Desktop/Bakalarka/BC/tika/tika-app/target/tika-app-2.0.0-SNAPSHOT.jar "$f"
done
dur2=$(echo "$(date +%s.%N) - $start" | bc)
echo "
Execution time for $i files in $FILES:
        ./parse.sh = $dur1 seconds
        tika-app-2.0.0-SNAPSHOT.jar = $dur2 seconds"


