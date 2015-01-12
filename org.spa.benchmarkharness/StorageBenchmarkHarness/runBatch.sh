#!/bin/bash
database='XXX'
configpath='XXX'
configname='XXX'
limit=XXX
for (( i=1; i<=$limit; i++ ))
do
  config="${configname}${i}.configuration"
  echo "[Batch] Running Configuration ${config}"
  cmd="./run -d ${database} -c ${configpath}${config} -r raw"
  echo "[Batch] Executing: ${cmd}"
  $cmd
done
