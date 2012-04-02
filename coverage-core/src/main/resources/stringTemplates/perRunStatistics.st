perRunStatistics(stats) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Coverage report for $stats.testName$</title>
<style>
    body {
        margin: 0;
        padding: 18px 10px;
        font-family: Arial, sans-serif;
        font-size: 14px;
    }

    table {
        border-collapse: collapse;
    }

    th, td.statements, td.executed, td.coverage {
        text-align: center;
    }

    th, td {
        padding: 2px;
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
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Total</td>
                <td class="statements">$stats.totalStatements$</td>
                <td class="executed">$stats.totalExecuted$</td>
                <td class="coverage">$stats.totalCoverage$%</td>
            </tr>
            $stats.fileStats:perFileStatistics(); separator="\n"$
        </tbody>
    </table>
</body>
</html>
>>