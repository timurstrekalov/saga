runStatsCsv(stats) ::= <<
File,Statements,Executed,Coverage
Total,$stats.totalStatements$,$stats.totalExecuted$,$stats.totalCoverage$%
$stats.fileStatsWithSeparateFileOnly:fileStatsCsv(); separator="\n"$
>>