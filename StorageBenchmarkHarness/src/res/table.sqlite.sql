CREATE TABLE IF NOT EXISTS {{TEMPLATE}}IndependentVars(runId INTEGER PRIMARY KEY);

CREATE TABLE IF NOT EXISTS {{TEMPLATE}}DependentVars(
	dvId INTEGER PRIMARY KEY,
	runId INTEGER NOT NULL,
	benchPrefix VARCHAR NOT NULL
);
	
CREATE INDEX IF NOT EXISTS {{TEMPLATE}}DependentVarsRunIdIdx ON {{TEMPLATE}}DependentVars(runId);

CREATE TABLE IF NOT EXISTS {{TEMPLATE}}DependentVarsValues(
	valueId INTEGER PRIMARY KEY,
	dvId INTEGER NOT NULL,
	operation VARCHAR NOT NULL,
	opMetric INTEGER NOT NULL,
	opValue REAL NOT NULL,
	opTimestamp INTEGER,
	opType INTEGER,
	source VARCHAR,
	FOREIGN KEY(dvId) REFERENCES {{TEMPLATE}}DependentVars(dvId)
);

CREATE INDEX IF NOT EXISTS {{TEMPLATE}}DependentVarsValuesRunIdIdx ON {{TEMPLATE}}DependentVarsValues(valueId);