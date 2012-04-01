perRunStatistics(stats) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Coverage report for $stats.testName$</title>
<style>
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
                <td>$stats.totalStatements$</td>
                <td>$stats.totalExecuted$</td>
                <td>$stats.totalCoverage$%</td>
            </tr>
            $stats.fileStats:perFileStatistics(); separator="\n"$
        </tbody>
    </table>
</body>
</html>
>>