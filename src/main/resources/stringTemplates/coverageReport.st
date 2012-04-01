coverageReport(lines) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Coverage</title>
<style>
    body {
        padding: 18px 10px;
        margin: 0;
    }

    table {
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
        padding: 1px 4px;
        margin: 0px 8px;
    }

    th, td, pre {
        padding: 0;
        margin: 0;

        font-size: 14px;
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
    <table>
        $lines:lineCoverage(); separator="\n"$
    </table>
</body>
</html>
>>
