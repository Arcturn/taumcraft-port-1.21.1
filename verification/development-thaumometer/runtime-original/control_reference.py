import json, sys, time, uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parent
request=json.load(sys.stdin)
requests=request if isinstance(request,list) else [request]
for request in requests:
    request['id']=uuid.uuid4().hex
    target=ROOT/'ui-command.json'
    assert not target.exists(),'Previous command pending'
    target.write_text(json.dumps(request)+'\n')
    deadline=time.monotonic()+35
    while time.monotonic()<deadline:
        result_path=ROOT/'ui-result.json'
        if result_path.exists():
            try:result=json.loads(result_path.read_text())
            except json.JSONDecodeError:result={}
            if result.get('id')==request['id']:
                print(json.dumps(result),flush=True)
                assert not result.get('error') and result.get('exit_code',0)==0
                break
        time.sleep(.1)
    else:raise TimeoutError('Reference controller did not respond')
    if request['action']=='probe':
        deadline=time.monotonic()+8
        while (ROOT/'command.txt').exists() and time.monotonic()<deadline:time.sleep(.05)
    time.sleep(request.get('wait',.2))
