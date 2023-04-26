import uvicorn
from fastapi import FastAPI, Request
import orjson
import firebase_admin
from firebase_admin import credentials, db

cred = credentials.Certificate("serviceAccountKey.json")

firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://giveandtake-31249-default-rtdb.firebaseio.com'
})
app = FastAPI()



@app.get("/")
async def root():
    return "Hello World"

@app.post('/')
async def submit(request: Request):
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    subject = data['subject']
    contactDetails= data['contactDetails']
    locationLat= data['locationLat']
    locationLang= data['locationLang']
    creationTime= data['creationTime']
    body= data['body']
    print(userId)
    users_ref = db.reference('users/')
    request_ref = users_ref.child(userId).child("requestId").child(requestId)
    request_ref.update({
        'subject': subject,
        'contactDetails': contactDetails,
        'body': body,
        'creationTime': creationTime
    })
    request_location_ref= request_ref.child('location')
    request_location_ref.update({
        'latitude': float(locationLat),
        'longitude': float(locationLang)
    })
    return 'success'


@app.post('/delete/')
async def delete(request: Request):
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    users_ref = db.reference('users/')
    users_ref.child(userId).child("requestId").child(requestId).delete()
    return 'success'

@app.post('/report/')
async def report(request: Request):
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestUserId= data['requestUserId']
    users_ref = db.reference('users/')
    fullName= users_ref.child(userId).child("fullName")
    requestId = data['requestId']
    reported_requests_ref = db.reference('reportedRequests/')
    report_ref= reported_requests_ref.child(requestId).child("reporters")
    report_ref.update({userId: fullName})
    report_ref= reported_requests_ref.child(requestId)
    report_ref.update({"requestUserId", requestUserId})
    return 'success'

@app.post('/unReport/')
async def unReport(request: Request):
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    reported_requests_ref = db.reference('reportedRequests/')
    report_ref= reported_requests_ref.child(requestId).child("reporters")
    report_ref.child(userId).delete()
    if report_ref.child() is None:   #if no users report this anymore, remove request from reported list
        report_ref = reported_requests_ref.child(requestId).delete()
    return 'success'

@app.post('/unJoin/')
async def unJoin(request: Request):
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    requestUserId = data['requestUserId']
    users_ref = db.reference('users/')
    users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners").child(
        userId).getRef().removeValue();
    users_ref.child(userId).child("requestsUserJoined").child(requestId).getRef().removeValue();
    return 'success'

@app.post('/join/')
async def join(request: Request):
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    requestUserId = data['requestUserId']
    joinerContactDetails = data['joinerContactDetails']
    users_ref = db.reference('users/')
    users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners").child(
        userId).child("contactDetails").setValue(joinerContactDetails);
    users_ref.child(userId).child("requestsUserJoined").child(requestId).child(
        "requestUserId").setValue(requestUserId);

    return 'success'

if __name__ == "__main__":
    uvicorn.run(app, host="10.102.2.155", port=8000)

