target_bin=/root/.volc/bin
if [ -d ~/.volc/.bash_completion.d/ ];then
  for bcfile in ~/.volc/.bash_completion.d/* ; do
    [ -f "$bcfile" ] && . "$bcfile"
  done
fi

if [[ ":$PATH:" != *":$target_bin:"* ]]; then
  if [ -f "/usr/local/bin/volc" ]; then # if old volc exists, prepend $target_bin
    export PATH=$target_bin:$PATH
  else
    export PATH=$PATH:$target_bin
  fi
fi
