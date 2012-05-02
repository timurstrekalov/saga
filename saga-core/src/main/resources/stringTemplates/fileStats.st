fileStats(fileStats) ::= <<
<tr $if(!fileStats.hasStatements)$class="no-statements"$endif$ onclick="__coverage_toggleSection('$fileStats.id$'); return false;">
    <td><a href="#">$if(fileStats.parentName)$$fileStats.parentName$/$endif$<strong>$fileStats.fileName$</strong></a></td>
    <td class="statements">$fileStats.statements$</td>
    <td class="executed">$fileStats.executed$</td>
    <td class="coverage">
        $if(fileStats.hasStatements)$
            $fileStats.coverage$&thinsp;%
        $else$
            -
        $endif$
    </td>
    <td class="bar-container">
        $if(fileStats.hasStatements)$
            <div class="bar">
                <span style="width: $fileStats.coverage$%;"></span>
            </div>
        $endif$
    </td>
</tr>
<tr id="__src_$fileStats.id$" style="display: none;">
    <td class="line-coverage-cell" colspan="5">$lineByLineCoverageData(fileStats)$</td>
</tr>
>>