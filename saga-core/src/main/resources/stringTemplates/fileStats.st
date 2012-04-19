fileStats(fileStats) ::= <<
<tr onclick="__coverage_toggleSection('$fileStats.id$'); return false;">
    <td><a href="#">$if(fileStats.parentName)$$fileStats.parentName$/$endif$<strong>$fileStats.fileName$</strong></a></td>
    <td class="statements">$fileStats.statements$</td>
    <td class="executed">$fileStats.executed$</td>
    <td class="coverage">$fileStats.coverage$%</td>
    <td class="bar-container">
        <div class="bar">
            <span style="width: $fileStats.coverage$%;"></span>
        </div>
    </td>
</tr>
<tr id="__src_$fileStats.id$" style="display: none;">
    <td class="line-coverage-cell" colspan="5">$lineByLineCoverageData(fileStats)$</td>
</tr>
>>