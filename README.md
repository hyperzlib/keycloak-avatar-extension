This is an updated keycloak avatar plugin compare from [thomasdarimont](https://github.com/mai1015/keycloak-avatar-minio-extension) and [mai1015](https://github.com/mai1015/keycloak-avatar-extension)

## Api
### Basic Parameters
| Key    | Type                                                    | Description      | Default |
|--------|---------------------------------------------------------|------------------|---------|
| size   | ```enum<String>("xs", "sm", "md", "lg", "xl", "xxl")``` | Avatar file size | lg      |
| format | ```enum<String>("raw", "json")```                       | Response format  | raw     |

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
