fileStatsCsv(fileStats) ::= <<
$if(fileStats.parentName)$$fileStats.parentName$/$endif$$fileStats.fileName$,$fileStats.statements$,$fileStats.executed$,$fileStats.coverage$%
>>