fileStatsRaw(fileStats) ::= <<
SF:$fileStats.filePath$
$fileStats.executableLineCoverageRecords:lineCoverageDataRaw(); separator="\n"$
end_of_record
>>