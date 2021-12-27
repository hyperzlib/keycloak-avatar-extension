This is an updated keycloak avatar plugin compare from [thomasdarimont](https://github.com/mai1015/keycloak-avatar-minio-extension) and [mai1015](https://github.com/mai1015/keycloak-avatar-extension)

## Config
### spi.avatar-provider.config.
| Key            | Type          | Description                                | Default     |
| -------------- | ------------- | ------------------------------------------ | ----------- |
| defaultAvatar  | ```string```  | URL for default avatar                     | ```/```     |
| alwaysRedirect | ```boolean``` | Should always redirect to static image url | ```false``` |
| defaultSize    | ```string```  | Default avatar size                        | ```lg```    |
| storageService | ```string```  | Provider to store avatar files             | ```file```  |

### spi.avatar-storage.file.
| Key            | Type         | Description                                   | Default                                             |
| -------------- | ------------ | --------------------------------------------- | --------------------------------------------------- |
| root           | ```string``` | FS storage root path                          | ```/```                                             |
| route          | ```string``` | URL and path to avatar file (Not need change) | ```/{realm}/avatar/{avatar_id}/avatar-{size}.png``` |
| baseurl        | ```string``` | Keycloak base url                             | ```/realms/```                                      |
| default-avatar | ```string``` | Path to default avatar                        | ```/{realm}/avatar/default.png```                   |

## Api
### Basic Parameters
| Key    | Type                                                    | Description      | Default                    |
| ------ | ------------------------------------------------------- | ---------------- | -------------------------- |
| size   | ```enum<String>("xs", "sm", "md", "lg", "xl", "xxl")``` | Avatar file size | ```{Config.defaultSize}``` |
| format | ```enum<String>("raw", "json")```                       | Response format  | ```raw```                  |

### Get avatar by user id
Request:
```
GET /realms/{realm}/avatar/by-userid/{user_id}
```

Respose (While format=json):
```js
{
    status: 1,
    avatar: "", // avatar url for lg
    avatar_tpl: "", // avatar url template, placeholder: %s
}
```

### Get avatar by username
Request:
```
GET /realms/{realm}/avatar/by-username/{username}
```

Respose (While format=json):
```js
{
    status: 1,
    avatar: "", // avatar url for lg
    avatar_tpl: "", // avatar url template, placeholder: %s
}
```

### Get avatar for current logined user
Warning: This api should only be used in frontend of keycloak.

Request:
```
GET /realms/{realm}/avatar/by-username/{username}
```

Respose (While format=json):
```js
{
    status: 1,
    avatar: "", // avatar url for lg
    avatar_tpl: "", // avatar url template, placeholder: %s
}
```

### Get default avatar
Request:
```
GET /realms/{realm}/avatar/default
```

Respose (While format=json):
```js
{
    status: 1,
    avatar: "", // avatar url for lg
    avatar_tpl: "", // avatar url template, placeholder: %s
}
```

### Set user avatar
Request:
```
POST {Any endpoint for GET}

image=<image file>
```
