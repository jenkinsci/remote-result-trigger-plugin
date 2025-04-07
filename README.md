Remote-Build-Result-Trigger-Plugin
===================================

A plugin for Jenkins CI that gives you the ability to monitor successful build on a **remote** Jenkins server.

## Scenes

Build project and pull docker images on a remote server（having public ip）, Customer didn't having a public ip and we
dont want to share source code.

![](screenshots/2023021501.png)

## Configuration

### Add Remote Server

Manage Jenkins > Configure System > Remote Result Trigger Configuration > Remote Jenkins Server  
![](screenshots/20230221153031.png)

### Set Build Trigger

Job > Build Triggers > Checked Remote Build Result Trigger  
![](screenshots/20250401101611.png)

## Publish

### Publish with pipeline

```groovy
pubResult(result: '{"version": "1.0.0"}')
```

### Publish with FreestyleProject

![](screenshots/20240127083709.png)

## Read

### Read remote jobs

```groovy
def remoteJobs = readRemoteJobs()
remoteJobs.each {
    echo readRemoteResult(uid: "${it.uid}").version
    // other
    echo "${it.jobUrl}"
}
```

### Read remote result

```groovy
// only one trigger job
readRemoteResult().version
// more than one
readRemoteResult(uid: 'xxxxxx').version
```

### envs

| ENV_NAME                           | description                                                                                                                                       |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| REMOTE_BUILD_NUMBER                | The remote build number, such as "153".                                                                                                           |  
| REMOTE_BUILD_TIMESTAMP             | The remote build timestamp, such as "1676017363424".                                                                                              |
| REMOTE_BUILD_URL                   | Full URL of this build, like [https://server:port/jenkins/job/foo/15/](https://server:port/jenkins/job/foo/15/) (<i>Jenkins URL</i> must be set). |
| REMOTE_BUILD_RESULT                | The remote build result, such as "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED".                                                        |
| REMOTE_PARAMETER_${PARAMETER_NAME} | If remote job use buildWithParameters, use remote parameters replace ${PARAMETER_NAME}.                                                           |
| REMOTE_RESULT_${PUB_RESULT_KEY}    | If remote use pubResult, use result json key replace ${PUB_RESULT_KEY}                                                                            |

If monitor more than one remote job
> The **Unique Identifier(Optional)** used to replace injected envs

| ENV_NAME                                   | description                                                                                                                                       |
|--------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| REMOTE_JOBS                                | ex: ["Unique Identifier(Optional)"]                                                                                                               |
| REMOTE_${uid}_BUILD_NUMBER                 | The remote build number, such as "153".                                                                                                           |  
| REMOTE_${uid}_BUILD_TIMESTAMP              | The remote build timestamp, such as "1676017363424".                                                                                              |
| REMOTE_${uid}_BUILD_URL                    | Full URL of this build, like [https://server:port/jenkins/job/foo/15/](https://server:port/jenkins/job/foo/15/) (<i>Jenkins URL</i> must be set). |
| REMOTE_${uid}_BUILD_RESULT                 | The remote build result, such as "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED".                                                        |
| REMOTE_${uid}\_PARAMETER_${PARAMETER_NAME} | If remote job use buildWithParameters, use remote parameters replace ${PARAMETER_NAME}.                                                           |
| REMOTE_${uid}\_RESULT_${PUB_RESULT_KEY}    | If remote use pubResult, use result json key replace ${PUB_RESULT_KEY}                                                                            |
