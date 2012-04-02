runStats(stats) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Coverage report for $stats.runName$</title>
<style>
    body {
        margin: 0;
        padding: 18px 10px;
        font: 0.8125em Verdana, sans-serif;
    }

    table {
        border-collapse: collapse;
        width: 100%;
    }

    th, td.statements, td.executed, td.coverage {
        text-align: center;
    }

    th, td {
        padding: 4px 10px;
    }

    th {
        font-size: 1.1em;
    }

    thead tr:first-child {
        background-color: #ccc;
    }

    th:first-child, td:first-child {
        border-top-left-radius: 4px;
        border-bottom-left-radius: 4px;
    }

    th:last-child, td:last-child  {
        border-top-right-radius: 4px;
        border-bottom-right-radius: 4px;
    }

    tbody tr:nth-child(odd) {
        background-color: #eee;
    }

    a, a:hover, a:visited, a:active {
        color: #0000ff;
    }

    td.coverage-bar {
        padding: 2px;
        padding-right: 8px;
        width: 15%;
    }

    .bar {
        border-radius: 8px;
        background-color: #555;

        padding: 2px;

        position: relative;
        height: 16px;
        width: 100%;
    }

    .bar > span {
        background-color: #00dd00;

        border-radius: 6px;

        display: block;
        height: 100%;
    }
</style>
</head>
<body>
    <table>
        <thead>
            <tr>
                <th>File</th>
                <th>Statements</th>
                <th>Executed</th>
                <th>Coverage</th>
                <th></th>
            </tr>
            <tr>
                <td>Total</td>
                <td class="statements">$stats.totalStatements$</td>
                <td class="executed">$stats.totalExecuted$</td>
                <td class="coverage">$stats.totalCoverage$%</td>
                <td class="coverage-bar">
                    <div class="bar">
                        <span style="width: $stats.totalCoverage$%;"></span>
                    </div>
                </td>
            </tr>
        </thead>
        <tbody>
            $stats.fileStats:fileStats(); separator="\n"$
        </tbody>
    </table>
</body>
</html>
>>