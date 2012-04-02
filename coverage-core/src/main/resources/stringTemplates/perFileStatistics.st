perFileStatistics(fileStats) ::= <<
<tr>
    <td><a href="$fileStats.href$">$fileStats.name$</a></td>
    <td class="statements">$fileStats.statements$</td>
    <td class="executed">$fileStats.executed$</td>
    <td class="coverage">$fileStats.coverage$%</td>
    <td class="coverage-bar">
        <div class="bar">
            <span style="width: $fileStats.coverage$%;"></span>
        </div>
    </td>
</tr>
>>