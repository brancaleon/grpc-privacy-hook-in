gRPC Hook-In
==============================================

Benchmarking with ghz tool:
```
./ghz -c 100 -n 1000 --insecure --proto ../../proto/order.proto --call proto.OrderService/OrderMeal -d '{"name":"Joe", "surname": "Tai", "address": "Strasse des 17. Juni", "meal": "currywurst"}' -m '{"clientId":"client", "Authorization":"Bearer [token]"}' localhost:50002
```
To add to

```
./ghz -c 100 -n 10000 --insecure --proto ../../proto/order.proto --call proto.OrderService/OrderMeal -d '{"name":"Joe", "surname": "Tai", "address": "Strasse des 17. Juni", "meal": "currywurst"}' -m '{"clientId":"client", "Authorization":"Bearer eyJhbGciOiJSUzUxMiJ9.eyJqdGkiOiI1MWNiN2I0OC00MWYxLTRmZTgtYTA3Mi1jNmU5N2E5NmI1NzQiLCJwdXJwb3NlIjoibWVhbF9wdXJjaGFzZSIsInN1YiI6ImNsaWVudCIsImlhdCI6MTY1NjU4MzU0MiwiZXhwIjoxNjU2NjEyMzQyfQ.DETCnCnFALvIjBzDd-sjoep2syJKa3_2NG3OXepe-6wIPwhX-mNjuRlEu9LLjDyB3J4hrUVAIAFjAq_kJcD8mSy_Kjevjks4jxX712i1l_nuSkmNI9mkow_Yc5afXdKiecMwFh2E3PTVghBUcPDjRbao2ZjEY9VZtc5UY6PM3W1fi0VmCsW0h5McnWKsZuhHdNEz9WaNDIqRYcrTxv8H4c4sCfW3tgu_828CJCH1AlItoV7enTqKqvxbNca4jQCAxW6RAHuqbDpxGvsluZUEziSMRt1cgVBF-eU1hoJuZVFF7BeDYPLGSzNUMdk8bUmJ14IpJUQ7EEVs-v2xAnbkdg"}' -O json localhost:50002 | http POST 127.0.0.1/api/projects/1/ingest
```
