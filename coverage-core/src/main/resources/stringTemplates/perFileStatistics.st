perFileStatistics(fileStats) ::= <<
<tr>
    <td><a href="$fileStats.href$">$fileStats.name$</a></td>
    <td class="statements">$fileStats.statements$</td>
    <td class="executed">$fileStats.executed$</td>
    <td class="coverage">$fileStats.coverage$%</td>
</tr>
>>