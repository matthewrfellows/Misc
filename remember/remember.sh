#!/bin/bash

notes_path="$HOME/Documents/Notes.txt"

find_today () {
    read dn m d t x Y <<< $(date)
    #local s=$(cat $notes_path | sed -n -E "/$dn $m $d .+ [A-Z]+ $Y/ p")
    local s=$(cat $notes_path | sed -n "/$dn $m $d $Y/ p")
    echo "$s"    
}
today=$(date -j "+%a %b %d %Y")
yesterday=$(date -j -v-1d "+%a %b %d %Y")
found_today="$(find_today)"
time_str="  - $(date -j "+%H:%M:%S %Z") -"
date_str="---- $today ----"


if [ $# -eq 0 ]; then
    read var_input
    if [ -n "$var_input" ]; then
        #echo "---- $date_str ----" >> "${notes_path}"
       if [ -z "$found_today" ]; then
           echo "$date_str" >> "${notes_path}"
       fi
       echo "$time_str" >> "${notes_path}"
       echo "$var_input" >> "${notes_path}"
    fi
elif [ $1 == "-e" ]; then
    if [ -z "$found_today" ]; then
        echo "$date_str" >> "${notes_path}"
    fi
    echo "$time_str" >> "${notes_path}"
    emacs "${notes_path}" -f end-of-buffer
elif [ $1 == "-o" ]; then
    emacs "${notes_path}" -f end-of-buffer
elif [ $1 == "-c" ]; then
    cat "${notes_path}"
elif [ $1 == "-y" ]; then
    cat "${notes_path}" | grep -A 10000 "$yesterday"
elif [ $1 == "-t" ]; then
    cat "${notes_path}" | grep -A 10000 "$today"
else
    cat "${notes_path}" | grep -i -A 5 "$1"
fi
