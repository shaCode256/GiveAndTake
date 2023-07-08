import json

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

#remember to connect to Wireless LAN adapter Wi-Fi:  IPv4 Address. . . . . . . . . . . : 10.0.0.3 for example
#and connect your phone to the same Wi-Fi as the PC server and app

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
    print("am deleting")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    users_ref = db.reference('users/')
    print("userId:"+userId)
    print("requestId:"+requestId)
    request_to_delete_ref= users_ref.child(userId).child("requestId").child(requestId)
    request_to_delete_ref.delete()

    #remove from all joiners list
    joiners_list= users_ref.child(userId).child("requestId").child(requestId).child(
            "joiners")
    if joiners_list != None and joiners_list.get() != None:
        joiners_list= joiners_list.get().val().keys()
        for joinerId in joiners_list:
            print(joinerId)
            # delete requestId from joiner's list
            db.reference("reportedRequests").child(requestId).delete()
            users_ref.child(joinerId).child("requestsUserJoined").child(requestId).delete()
    return 'success'

@app.post('/report/')
async def report(request: Request):
    body = await request.body()
    print("am reporting")
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestUserId= data['requestUserId']
    requestId = data['requestId']
    print('userId:'+userId+",requestUserId:"+requestUserId+",requestId:"+requestId)
    users_ref = db.reference('users/')
    fullName= users_ref.child(userId).child('fullName').get()
    reported_requests_ref = db.reference('reportedRequests/')
    report_ref= reported_requests_ref.child(requestId).child("reporters")
    request_ref= reported_requests_ref.child(requestId)
    report_ref.update({userId: fullName})
    request_ref.update({'requestUserId': requestUserId})
    return 'success'

@app.post('/unReport/')
async def unReport(request: Request):
    print("clicked unreport")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    reported_requests_ref = db.reference('reportedRequests/')
    report_ref= reported_requests_ref.child(requestId).child("reporters")
    request_report_ref= reported_requests_ref.child(requestId)
    report_ref.child(userId).delete()
    if report_ref.get() is None:   #if no users report this anymore, remove request from reported list
       request_report_ref.delete()
       print("request is no longer reported by any user")
    return 'success'

@app.post('/unJoin/')
async def unJoin(request: Request):
    print("enter unjoined")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    requestUserId = data['requestUserId']
    users_ref = db.reference('users/')
    users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners").child(
        userId).delete()
    users_ref.child(userId).child("requestsUserJoined").child(requestId).delete()
    return 'success'

@app.post('/join/')
async def join(request: Request):
    print("enter join")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId= data['userId']
    requestId = data['requestId']
    requestUserId = data['requestUserId']
    users_ref = db.reference('users/')
    joinerContactDetails = users_ref.child(userId).child('email').get()
    users_ref.child(requestUserId).child("requestId").child(requestId).child("joiners").child(
        userId).child("contactDetails").set(joinerContactDetails)
    users_ref.child(userId).child("requestsUserJoined").child(requestId).child(
        "requestUserId").set(requestUserId)
    return 'success'

@app.post('/getRequestDetails/')
async def getRequestDetails(request: Request):
    print("enter getRequestDetails")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    requestUserId = data['requestUserId']
    requestId = data['requestId']
    users_ref = db.reference('users/')
    requestSubject = users_ref.child(requestUserId).child("requestId").child(requestId).child("subject").get()
    requestBody = users_ref.child(requestUserId).child("requestId").child(requestId).child("body").get()
    contactDetails = users_ref.child(requestUserId).child("requestId").child(requestId).child("contactDetails").get()
    requestLatitude = str(users_ref.child(requestUserId).child("requestId").child(requestId).child("location").child("latitude").get())
    requestLongitude = str(users_ref.child(requestUserId).child("requestId").child(requestId).child("location").child("longitude").get())
    creationTime = users_ref.child(requestUserId).child("requestId").child(requestId).child("creationTime").get()

    result= {"requestBody": requestBody, "requestSubject": requestSubject, "contactDetails": contactDetails,
            "requestLongitude": requestLongitude, "requestLatitude": requestLatitude, "creationTime": creationTime
    }
   # result= result[1:-1]
    print(result)
    return result

@app.post('/setKmDistanceNotifications/')
async def setKmDistanceNotifications(request: Request):
    print("enter setKmDistanceNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    distance= data['distance']
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("distance").set(distance);
    return 'success'

@app.post('/turnOnOffAutoDetectLocationNotifications/')
async def turnOnAutoDetectLocationBtn(request: Request):
    print("enter turnOnOffAutoDetectLocationNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    onOff = int( data['onOff'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("autoDetectLocation").set(onOff);
    return 'success'

@app.post('/useSpecificLocationNotifications/')
async def useSpecifiedLocation(request: Request):
    print("enter useSpecifiedLocation")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    latitude = float(data['latitude'])
    longitude = float(data['longitude'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("autoDetectLocation").set(0);
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('latitude').set(latitude)
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('longitude').set(longitude)
    return 'success'

@app.post('/turnOnOffNotifications/')
async def turnOnNotifications(request: Request):
    print("enter turnOnOffNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    onOff = int (data['onOff'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("turnedOn").set(onOff);
    return 'success'

@app.post('/useCurrLocationNotifications/')
async def useCurrLocationNotifications(request: Request):
    print("enter turnOnOffNotifications")
    body = await request.body()
    body.decode("utf-8")
    data = orjson.loads(body)
    userId = data['userId']
    latitude = float(data['latitude'])
    longitude = float(data['longitude'])
    users_ref = db.reference('users/')
    users_ref.child(userId).child("settings").child("notifications").child("autoDetectLocation").set(0)
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('latitude').set(latitude)
    users_ref.child(userId).child("settings").child("notifications").child("specificLocation").child('longitude').set(longitude)
    return 'success'

if __name__ == "__main__":
    uvicorn.run(app, host="10.0.0.3", port=8000)

