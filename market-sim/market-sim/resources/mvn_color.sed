# Build Success
/BUILD SUCCESS$/{
    s/^/\x1b[32m/
    s/$/\x1b[m/
}
# Build Failure
/BUILD FAILURE$/{
    s/^/\x1b[31m/
    s/$/\x1b[m/
}
# Error
/^\[ERROR\]/{
    s/^/\x1b[31m/
    s/$/\x1b[m/
}
# Warning
/^\[WARNING\]/{
    s/^/\x1b[33m/
    s/$/\x1b[m/
}
# All tests skipped
/^Tests run: ([0-9]+), Failures: 0, Errors: 0, Skipped: \1/{
    s/^/\x1b[2m/
    s/$/\x1b[m/
}
# All tests passed
/^Tests run: [0-9]+, Failures: 0, Errors: 0/{
    s/^/\x1b[32m/
    s/$/\x1b[m/
}
# Some tests failed
/^Tests run: [0-9]+/{
    /Failures: 0, Errors: 0/!{
        s/^/\x1b[31m/
        s/$/\x1b[m/
    }
}
