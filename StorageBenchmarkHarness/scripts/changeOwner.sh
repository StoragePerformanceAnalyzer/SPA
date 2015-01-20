#!/bin/bash
owner=$(ls -ld $1/.. | awk '{print $3}')
chown -hR $owner $1
