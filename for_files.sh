FILES="/home/rene/Desktop/Bakalarka/test_folder/Shimadzu/*"
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Mettler/*"
#FILES="/home/rene/Desktop/Bakalarka/ReTabula/extension/Modules/src/test/resources/test-data/SpecordCSV/*"
#FILES="/home/rene/Desktop/Bakalarka/ReTabula/extension/Modules/src/test/resources/test-data/AgilentXLSX/*"
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Dalsi_priklady/Letters/*"
#FILES="/home/rene/Desktop/Bakalarka/test_folder/Dalsi_priklady/Agilent/*"

for f in $FILES
do
  echo "Processing $f file..."
  # take action on each file. $f store current file name
  # shellcheck disable=SC2093
  # shellcheck disable=SC2210
  ./parse.sh "$f" > 1  && comm -12 <(uniq 1 | sort 1) <(uniq 2 | sort 2) > 3 && cat 3 > 2
#  ./parse.sh "$f"
done
uniq 2 > 4
# shellcheck disable=SC2210
