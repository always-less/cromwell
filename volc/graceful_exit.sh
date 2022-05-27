#pid=`pidof bash -o 1 | awk -F' ' '{print $NF}'`
pid=`pidof java`
echo $pid
kill -15 $pid
while [ -e /proc/$pid ]
do
    echo "still  there with pid $pid"
    sleep .6
done &&
echo "pre stop finished"
sleep 20