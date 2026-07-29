rootProject.name = "platform"

include("configuration")
include("common")
include("web")
include("contracts")

include("courses:application")
findProject(":courses:application")?.name = "courses-application"
include("courses:web")
findProject(":courses:web")?.name = "courses-web"

include("administration:application")
findProject(":administration:application")?.name = "administration-application"
include("administration:integration-events")
findProject(":administration:integration-events")?.name = "administration-integration-events"
include("administration:web")
findProject(":administration:web")?.name = "administration-web"

include("course-enrollments:application")
findProject(":course-enrollments:application")?.name = "course-enrollments-application"
include("course-enrollments:web")
findProject(":course-enrollments:web")?.name = "course-enrollments-web"

include("course-reviews:application")
findProject(":course-reviews:application")?.name = "course-reviews-application"
include("course-reviews:web")
findProject(":course-reviews:web")?.name = "course-reviews-web"

include("users:application")
findProject(":users:application")?.name = "users-application"
include("users:web")
findProject(":users:web")?.name = "users-web"

include("security:config")
findProject(":security:config")?.name = "security-config"
include("security:test")
findProject(":security:test")?.name = "security-test"
