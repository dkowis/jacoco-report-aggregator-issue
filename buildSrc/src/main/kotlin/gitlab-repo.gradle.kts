plugins {
    `java-library`
    `maven-publish`
}

val gitlabPrivateToken = findProperty("gitLabPrivateToken") as String?

val projectId = findProperty("projectId") as String?
var headerName: String? = "Private-Token"
var headerValue: String? = gitlabPrivateToken
if(System.getenv("CI_JOB_TOKEN") != null) {
    headerName =  "Job-Token"
    headerValue = System.getenv("CI_JOB_TOKEN")
}

//Might not have needed these, but it's still handyu
project.ext.set("gitlabHeaderName", headerName)
project.ext.set("gitlabHeaderValue", headerValue)
project.ext.set("projectId", projectId)

repositories {
    maven {
        url = uri("https://gitlab.com/api/v4/groups/10360152/-/packages/maven")
        name = "GitLab"
        credentials(HttpHeaderCredentials::class) {
            name = headerName
            value = headerValue
        }
        authentication {
            create("header", HttpHeaderAuthentication::class)
        }
    }
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://gitlab.com/api/v4/projects/${projectId}/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = headerName
                value = headerValue
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}
