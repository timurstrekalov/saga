runStatsCobertura(stats, name, version, url) ::= <<
<?xml version="1.0" ?>
<!DOCTYPE coverage SYSTEM 'http://cobertura.sourceforge.net/xml/coverage-04.dtd'>
<!-- Generated using $name$ ($url$) version $version$ -->
<coverage branch-rate="0.0" branches-covered="0" branches-valid="0" complexity="0" line-rate="$stats.totalCoverageRate$" lines-valid="$stats.totalStatements$" lines-covered="$stats.totalExecuted$" timestamp="1376875707" version="1.9">
	<sources>
		$stats.sourceDirs:sourceDirsCobertura(); separator="\n"$
	</sources>
	<packages>
		<package name="default" line-rate="0.0" branch-rate="0.0" complexity="0">
			<classes>
				$stats.fileStatsWithSeparateFileOnly:classStatsCobertura(); separator="\n"$
			</classes>
		</package>
	</packages>
</coverage>

>>