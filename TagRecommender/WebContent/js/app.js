Todos = SC.Application.create();

// SUGGESTION: Your tags can be stored in a similar way as how todos are stored
Todos.Todo = SC.Object.extend({
  title: null,
  isDone: false
});

Todos.Tag = SC.Object.extend({
  name: null,
  probability: 0.0
});

Todos.todosController = SC.ArrayProxy.create({
  content: [],

  createTodo: function(title) {
    var todo = Todos.Todo.create({ title: title });
    this.pushObject(todo);
  },

  clearCompletedTodos: function() {
    this.filterProperty('isDone', true).forEach(this.removeObject, this);
  },

  remaining: function() {
    return this.filterProperty('isDone', false).get('length');
  }.property('@each.isDone'),

  allAreDone: function(key, value) {
    if (value !== undefined) {
      this.setEach('isDone', value);

      return value;
    } else {
      return !!this.get('length') && this.everyProperty('isDone', true);
    }
  }.property('@each.isDone')
});

Todos.tagsController = SC.ArrayProxy.create({
  content: [{name: "#shop", probability: 0.5}],

  createTag: function(name, probability) {
    var tag = Todos.Tag.create({ name: name, probability: probability });
	this.pushObject(tag);
  },

  clearAllTags: function() {
    this.forEach(this.removeObject, this);
  },
  
  recommendTags: function(todo) {
	$.get('recommend',
	  {
		todo: todo
	  },
	  function(data){
	    Todos.tagsController.beginPropertyChanges();
	    Todos.tagsController.clearAllTags();
	    data.forEach(function(item){
	      item = item.tag;
	      var tag = Todos.Tag.create({
	        name: item.name,
	        probability: item.probability
	      });
	      Todos.tagsController.pushObject(tag);
	    });
	    Todos.tagsController.endPropertyChanges();
	  });
  }
});

Todos.StatsView = SC.View.extend({
  remainingBinding: 'Todos.todosController.remaining',

  remainingString: function() {
    var remaining = this.get('remaining');
    return remaining + (remaining === 1 ? " item" : " items");
  }.property('remaining')
});

// SUGGESTION: Think about using SC.TextArea to get a bigger input field
Todos.CreateTodoView = SC.TextField.extend({
  insertNewline: function() {
    var value = this.get('value');

    if (value) {
      Todos.todosController.createTodo(value);
      this.set('value', '');
    }
  },
  
  focusOut: function() {
	var value = this.get('value');
	
	if (value) {
		Todos.tagsController.recommendTags(value);
	}
  }
});

