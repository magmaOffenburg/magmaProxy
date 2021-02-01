#!/bin/bash
# ******************************************************************************
# * Copyright 2008, 2015 Hochschule Offenburg
# * Klaus Dorer, Stefan Glaser
# *
# * This file is part of magma Simspark Agent Proxy.
# *
# * Simspark Agent Proxy is free software: you can redistribute it and/or modify
# * it under the terms of the GNU General Public License as published by
# * the Free Software Foundation, either version 3 of the License, or
# * (at your option) any later version.
# *
# * Simspark Agent Proxy is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# * GNU General Public License for more details.
# *
# * You should have received a copy of the GNU General Public License
# * along with it. If not, see <http://www.gnu.org/licenses/>.
# ******************************************************************************
# Starts a simulation server proxy
# example: bash start.sh 127.0.0.1 3100 3110
# ******************************************************************************

if [ $# -ne 3 ]; then
        echo "Usage: $0 <Simspark server IP> <Simspark server Port> <Proxy server port>"
        exit 1
fi

cd "$(dirname "$0")"
java -jar magmaProxy.jar --server=$1 --serverport=$2 --proxyport=$3
