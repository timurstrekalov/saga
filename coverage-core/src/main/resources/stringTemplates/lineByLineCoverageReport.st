lineByLineCoverageReport(fileStats) ::= <<
<table id="$fileStats.href$" class="line-coverage" style="display: none;">
    $fileStats.lineCoverageRecords:lineCoverage()$
</table>
>>
