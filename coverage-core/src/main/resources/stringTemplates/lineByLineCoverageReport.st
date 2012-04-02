lineByLineCoverageReport(lines, stats) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Coverage report for $stats.name$</title>
<style>
    body {
        padding: 18px 10px;
        margin: 0;
        font: 0.8125em Verdana, sans-serif;
    }

    h1 {
        font-size: 1.2em;
        margin: 0;
        padding-left: 10px;
        padding-bottom: 5px;
    }

    h2 {
        font-size: 1em;
        font-weight: normal;
        margin: 0;
        padding: 2px 20px;
    }

    table {
        margin-top: 10px;
        border-collapse: collapse;
    }

    th {
        border-right: 1px solid #ccc;
    }

    th div, td div {
        text-align: center;
    }

    th div {
        margin: 0px 10px 0px 4px;
    }

    td div {
        padding: 1px 6px;
        margin: 0px 8px;
    }

    th, td, pre {
        padding: 0;
        margin: 0;

        font-size: 1.1em;
        font-weight: normal;
        font-family: Courier New;
    }

    th div, td.coverage div {
         border-radius: 8px;
    }

    td pre {
         margin-left: 8px;
    }

    tr.covered td.coverage div {
         background-color: lightgreen;
    }

    tr.not-covered td.coverage div {
         background-color: lightpink;
    }

    span.keyword {
        color: #000080;
        font-weight: bold;
    }

    span.string {
        color: #008000;
    }

    span.number {
        color: #0000ff;
    }
</style>
</head>
<body>
    <div class="header">
        <h1>$stats.name$</h1>
        <h2 class="statements">Statements: $stats.statements$</h2>
        <h2 class="executed">Executed: $stats.executed$</h2>
        <h2 class="coverage">Coverage: $stats.coverage$%</h2>
    </div>

    <table>
        $lines:lineCoverage(); separator="\n"$
    </table>
</body>
</html>
>>
