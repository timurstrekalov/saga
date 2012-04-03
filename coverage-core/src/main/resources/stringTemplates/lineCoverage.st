lineCoverage(record) ::= <<
<tr class="$record.cssClass$">
    <th>
        <div>$record.lineNr$</div>
    </th>
    <td class="times-executed">
        <div>
            $if(record.executable)$
                $record.timesExecuted$
            $else$
                &nbsp;
            $endif$
        </div>
    </td>
    <td>
        <pre>$record.line$</pre>
    </td>
</tr>
>>