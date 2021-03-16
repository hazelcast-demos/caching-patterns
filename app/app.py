import json
import os
from logging import basicConfig, INFO

from flask import Flask, jsonify, request, Response
from hazelcast import HazelcastClient

app = Flask(__name__)
basicConfig(level=INFO)
app.config['JSON_SORT_KEYS'] = False
client = HazelcastClient(
    cluster_members=[
        os.getenv('CLUSTER_HOST', 'localhost')
    ]
)
cache = client.get_map("persons").blocking()


@app.route('/')
def get_all():
    persons = cache.entry_set()
    return jsonify([json.loads(person) for key, person in persons])


@app.route('/<pk>')
def get_one(pk):
    person = cache.get(int(pk))
    if person is not None:
        return person
    else:
        return Response(status=404)


@app.route('/', methods=['POST'])
def post():
    data = request.get_json()
    key = data['id']
    cache.set(key, json.dumps(data, indent=4))
    return Response(status=201, headers={'Location': f'{request.url}{key}'})
