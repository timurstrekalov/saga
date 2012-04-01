perFileStatistics(fileStats) ::= <<
<tr>
    <td><a href="$fileStats.href$">$fileStats.name$</a></td>
    <td>$fileStats.statements$</td>
    <td>$fileStats.executed$</td>
    <td>$fileStats.coverage$%</td>
</tr>
>>