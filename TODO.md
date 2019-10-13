# TODO

## 1.0

- Only support CSV, leave HSQL code for now
- Implement CSV files setup if not found for easy user interaction
- Make README actually useful
- make standalone jar
  - create simple .sh
  - consider packaging jre
- implement command line args
  - CSV path, algorithm, decay time
- implement decay(happens when a player does not play for a while)
- figure out benefit of using hsql...
- clean up general code(logging, proper structuring of code (dateformatter))
  - refactor CSV code to be more functional and use it as a base for future *SQL implementations

## Near Future

- implement writeStore for Csv
  - implement active gplayer csv
- clean up and actually use HSQL implementation

## Far Future

- think of different GUI implementations
- depending on development, consider using a proper db library(Hibernate, or something good)
