runStatsCsv(stats) ::= <<
File,Statements,Executed,Coverage
$stats.fileStatsWithSeparateFileOnly:fileStatsCsv(); separator="\n"$
>>