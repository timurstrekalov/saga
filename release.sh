#!/bin/bash
mvn release:clean release:prepare -P all "$@"
