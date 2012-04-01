lineCoverage(it) ::= <<
<tr class="$it.cssClass$">
    <th>
        <div>$it.lineNr$</div>
    </th>
    <td class="coverage">
        <div>
            $if(it.executable)$
                $it.coverage$
            $else$
                &nbsp;
            $endif$
        </div>
    </td>
    <td>
        <pre>$it.line$</pre>
    </td>
</tr>
>>