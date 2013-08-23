classStatsCobertura(fileStats) ::= <<
<class name="$fileStats.fileName$" filename="$fileStats.relativeName$" line-rate="$fileStats.coverageRate$" branch-rate="0.0" complexity="0">
	<methods />
	<lines>
		$fileStats.executableLineCoverageRecords:lineCoverageDataCobertura(); separator="\n"$
	</lines>
</class>
>>